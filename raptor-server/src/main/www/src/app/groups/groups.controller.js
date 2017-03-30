export class GroupController {

  constructor($http, R, toastr) {
    'ngInject'

    this.groups = [];
    this.$http = $http;
    this.R = R;
    this.toastr = toastr;
  }

  refresh() {
    this.$http({
      method: 'GET',
      url: `${this.R.endpoint}/${this.R.auth.ns}/groups`
    }).then(res => {
      this.groups = res;
    }, err => {
      this.toastr.error('Load groups failed!', 'ERROR');
    });
  }

  create() {

  }

}
