import * as _ from "lodash";
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
    this.$http({
      method: 'GET',
      url: `${this.R.endpoint}/groups`,
      headers: this.R.headers
    }).then(res => {
      this.$log.info(res.data);
      this.groups = res.data;
      _.each(this.groups, it => {
        it.expired = new Date(_.now() + it['expires'] * 1000);
      });
    }, err => {
      this.toastr.error(err, 'ERROR');
    });
  }

  edit(gp) {
    this.current = _.cloneDeep(gp);
    angular.element('#modalGroup').modal('show');
  }

  create() {
    this.current = {
      isNew: true,
      name: `group_${_.uniqueId()}`,
      expires: 60,
      addresses: []
    };
    angular.element('#modalGroup').modal('show');
  }

  qrcode(gp) {
    this.$http({
      method: 'GET',
      url: `${this.R.endpoint}/groups/${gp.name}/publish`,
      headers: this.R.headers
    }).then(res => {
      this.$log.info(res.data);
      this.qr = res.data.url;
      angular.element('#modalQRCode').modal('show');
    }, err => {
      this.toastr.error(err, 'ERROR');
    });
  }

  save() {
    let emsg = this.errmsg();
    if (emsg) {
      this.$log.error(emsg);
      this.toastr.error(emsg, 'ERROR');
      return;
    }

    if (this.current.isNew) {
      let opts = {
        method: 'POST',
        url: `${this.R.endpoint}/groups/${this.current.name}`,
        headers: this.R.headers
      };
      this.$http(opts, this.current)
        .then(res => {
          this.$log.info(res.data);
        }, err => {
          this.toastr.error(err, 'ERROR');
        });
    } else {
      let opts = {
        method: 'PUT',
        url: `${this.R.endpoint}/groups/${this.current.name}`,
        headers: this.R.headers
      };
      this.$http(opts, this.current)
        .then(res => {
          this.$log.info(res.data);
        }, err => {
          this.toastr.error(err, 'ERROR');
        });
    }
  }

  errmsg() {
    let isNameValid = /^[a-zA-Z0-9_]+/g.test(this.current.name);
    if (!isNameValid) {
      return 'Illegal group name!';
    }

    if (!this.current.addresses || this.current.addresses.length < 1) {
      return 'Blank address list!';
    }
    let msg = undefined;
    let addrPattern = /^rtmp:\/\/([a-zA-Z0-9\\-_.]+)(:[1-9][0-9]+)?\/([a-zA-Z0-9_\\-]+)$/g;
    for (let i = 0; i < this.current.addresses.length; i++) {
      let address = this.current.addresses[i];
      if (!addrPattern.test(address.url)) {
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
