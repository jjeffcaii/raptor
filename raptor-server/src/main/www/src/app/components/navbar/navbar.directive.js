export function NavbarDirective() {
  'ngInject';

  let directive = {
    restrict: 'E',
    templateUrl: 'app/components/navbar/navbar.html',
    scope: {
      creationDate: '='
    },
    controller: NavbarController,
    controllerAs: 'vm',
    bindToController: true
  };

  return directive;
}

class NavbarController {
  constructor($state, $cookies) {
    'ngInject';
    this.$cookies = $cookies;
    this.$state = $state;
  }

  exit() {
    this.$cookies.remove('i');
    this.$cookies.remove('k');
    this.$state.go('home');
  }

}
