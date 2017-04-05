export class MainController {

  constructor($state, $http, R, toastr, $cookies, $log) {
    'ngInject';

    this.auth = {
      ns: '',
      pwd: ''
    };
    this.$state = $state;
    this.$http = $http;
    this.R = R;
    this.toastr = toastr;
    this.$cookies = $cookies;
    this.$log = $log;
  }


  init() {
    this.auth.ns = this.$cookies.get('i');
    this.auth.pwd = this.$cookies.get('k');
  }

  test() {
    this.auth.ns = '5795ad33aa150a0001fcbfa3';
    this.auth.pwd = 'QXNJZnpNRDBZejhfbmpwRjlBVk5Bdw';
  }

  login() {
    if (!this.auth.ns || !this.auth.pwd) {
      return;
    }
    let c = {
      headers: {
        'X-ML-AppId': this.auth.ns,
        'X-ML-APIKey': this.auth.pwd,
        'Content-Type': 'application/json; charset=utf-8'
      }
    };

    this.$http.get(`${this.R.endpoint}/ok`, c)
      .then(() => {
        this.$cookies.put('i', this.auth.ns);
        this.$cookies.put('k', this.auth.pwd);
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
