export class MainController {

  constructor($state, $http, R, toastr) {
    'ngInject';

    this.auth = {
      ns: '5795ad33aa150a0001fcbfa3',
      pwd: 'QXNJZnpNRDBZejhfbmpwRjlBVk5Bdw'
    };
    this.$state = $state;
    this.$http = $http;
    this.R = R;
    this.toastr = toastr;
  }

  login() {
    let c = {
      headers: {
        'X-ML-AppId': this.auth.ns,
        'X-ML-APIKey': this.auth.pwd,
        'Content-Type': 'application/json; charset=utf-8'
      }
    };

    this.$http.get(`${this.R.endpoint}/ok`, c)
      .then(() => {
        this.R.headers = c.headers;
        this.toastr.success('Log in success!', 'OK');
        this.$state.go('groups');
      })
      .catch(err => {
        this.toastr.error(err.data.msg, `ERROR-${err.data.code}`);
      });
  }

  validate() {
    return /[a-f0-9]{24}/.test(this.auth.ns) && this.auth.pwd;
  }

}
