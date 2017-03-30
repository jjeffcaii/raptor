export class MainController {

  constructor($state) {
    'ngInject';
    this.auth = {
      ns: '000000000000000000000000',
      pwd: 'iseedeadpeople'
    };
    this.$state = $state;
  }

  login() {
    this.$state.go('groups');
  }

  validate() {
    return /[a-f0-9]{24}/.test(this.auth.ns) && this.auth.pwd;
  }

}
