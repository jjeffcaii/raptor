export function GpDirective() {
  'ngInject';

  let directive = {
    restrict: 'E',
    templateUrl: 'app/components/gp/gp.html',
    scope: {
      gp: '=',
      tab: '='
    },
    controller: GpController,
    controllerAs: 'vm',
    bindToController: true
  };

  return directive;
}

class GpController {

  constructor($log) {
    'ngInject';
    this.logger = $log;
    this.tab = 0;
  }

  toTab(tabIndex) {
    this.tab = tabIndex;
  }

  newAddress() {
    this.gp.addresses.push({
      url: 'rtmp://YOUR_SERVER/YOUR_CONTEXT',
      streamKey: '?foo=bar'
    });
  }

  resetAddress() {
    this.gp.addresses = []
  }

  removeAddress(address) {
    let addresses = this.gp.addresses;
    let idx = addresses.indexOf(address);
    addresses.splice(idx, 1);
  }

}
