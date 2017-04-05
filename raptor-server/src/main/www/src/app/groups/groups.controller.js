import * as _ from "lodash";

const PATTERN_ADDR_URL = /rtmp:\/\/([a-zA-Z0-9_.\-]+)(:[1-9][0-9]+)?\/[a-zA-Z0-9_\-]+/;

export class GroupController {

  constructor($http, R, toastr, $log, $state, $cookies, $interval) {
    'ngInject';

    this.groups = [];
    this.$http = $http;
    this.R = R;
    this.toastr = toastr;
    this.$log = $log;
    this.$state = $state;
    this.$cookies = $cookies;
    this.$interval = $interval;
  }

  _headers() {
    let ns = this.$cookies.get('i');
    let tk = this.$cookies.get('k');
    if (!ns || !tk) {
      this.$state.go('home');
      this.toastr.error('Your session is expired!', 'ERROR');
      throw new Error('raptor session expired.')
    }
    return {
      'X-ML-AppId': ns,
      'X-ML-APIKey': tk,
      'Content-Type': 'application/json; charset=utf-8'
    };
  }

  fight() {
    if (!_.isUndefined(this.stop)) {
      return;
    }
    this.stop = this.$interval(() => {
      _.each(this.groups, gp => {
        if (--gp.expires < 1) {
          let idx = this.groups.indexOf(gp);
          this.groups.splice(idx, 1);
        }
      });
    }, 1000);
  }

  init() {
    this.refresh();
    this.fight();
  }

  stopFlight() {
    if (_.isUndefined(this.stop)) {
      return;
    }
    this.$interval.cancel(this.stop);
    this.stop = undefined;
  }

  refresh() {
    this.$http.get(`${this.R.endpoint}/groups`, {headers: this._headers()})
      .then(res => {
        this.groups = res.data;
        _.each(this.groups, it => {
          it.left = it.expires;

        });
      })
      .catch(err => {
        this.toastr.error(err.data.msg, `ERROR-${err.data.code}`);
      });
  }

  edit(gp) {
    this.current = _.cloneDeep(gp);
    angular.element('#modalGroup').modal('show');
  }

  drop(gp) {
    let opts = {
      url: `${this.R.endpoint}/groups/${gp.name}`,
      method: 'DELETE',
      headers: this._headers()
    };

    this.$http(opts).then(() => {
      this.toastr.success(`Delete stream group ${gp.name} success!`, 'Notification')
      this.refresh();
    }).catch(err => {
      this.toastr.error(err.data.msg, `ERROR-${err.data.code}`);
    })
  }

  create() {
    this.current = {
      isNew: true,
      name: `stream_${_.uniqueId()}`,
      expires: 300,
      addresses: []
    };
    angular.element('#modalGroup').modal('show');
  }

  qrcode(gp) {
    this.$http.get(`${this.R.endpoint}/groups/${gp.name}/publish`, {headers: this._headers()})
      .then(res => {
        this.$log.info(res.data);
        this.qr = res.data.url;
        angular.element('#modalQRCode').modal('show');
      }, err => {
        this.toastr.error(err.data.msg, `ERROR-${err.data.code}`);
      });
  }

  save() {
    let emsg = this.errmsg();
    if (emsg) {
      this.toastr.error(emsg, 'ERROR');
      return;
    }
    let api = `${this.R.endpoint}/groups/${this.current.name}`;
    let p;
    if (this.current.isNew) {
      p = this.$http.post(api, this.current, {headers: this._headers()})
    } else {
      p = this.$http.put(api, this.current, {headers: this._headers()})
    }

    p.then(res => {
      this.$log.info(res.data);
      this.toastr.success('Save group success!', 'INFO');
      this.refresh();
      angular.element('#modalGroup').modal('hide');
    }).catch(err => {
      this.toastr.error(err.data.msg, `ERROR-${err.data.code}`);
    });
  }

  errmsg() {
    if (!this.current) {
      return 'NULL'
    }
    let isNameValid = /^[a-zA-Z0-9_]+/g.test(this.current.name);
    if (!isNameValid) {
      return 'Illegal group name!';
    }
    if (!this.current.addresses || this.current.addresses.length < 1) {
      return 'Empty addresses!';
    }
    let msg = undefined;
    for (let i = 0, len = this.current.addresses.length; i < len; i++) {
      let address = this.current.addresses[i];
      if (!address.url) {
        msg = 'Blank address url!';
        break;
      } else if (!PATTERN_ADDR_URL.test(address.url)) {
        msg = `Illegal address url: ${address.url}!`;
        break;
      }
      if (!address.streamKey) {
        msg = 'Address stream key is blank!';
        break;
      }
    }
    return msg;
  }

}
