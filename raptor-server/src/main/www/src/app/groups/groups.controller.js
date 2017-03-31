import * as _ from "lodash";

const PATTERN_ADDR_URL = /rtmp:\/\/([a-zA-Z0-9_.\-]+)(:[1-9][0-9]+)?\/[a-zA-Z0-9_\-]+/;

export class GroupController {

  constructor($http, R, toastr, $log, moment) {
    'ngInject'

    this.groups = [];
    this.$http = $http;
    this.R = R;
    this.toastr = toastr;
    this.$log = $log;
    this.$moment = moment;
  }

  refresh() {
    this.$http.get(`${this.R.endpoint}/groups`, {headers: this.R.headers})
      .then(res => {
        this.groups = res.data;
        _.each(this.groups, it => {
          it.expired = new Date(_.now() + it['expires'] * 1000);
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
    this.$http.get(`${this.R.endpoint}/groups/${gp.name}/publish`, {headers: this.R.headers})
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
      p = this.$http.post(api, this.current, {headers: this.R.headers})
    } else {
      p = this.$http.put(api, this.current, {headers: this.R.headers})
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
