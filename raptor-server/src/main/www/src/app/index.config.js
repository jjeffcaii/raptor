export function config($logProvider, toastrConfig, cfpLoadingBarProvider, $httpProvider) {
  'ngInject';
  // Enable log
  $logProvider.debugEnabled(true);

  // Set options third-party lib
  toastrConfig.allowHtml = false;
  toastrConfig.timeOut = 300;
  toastrConfig.positionClass = 'toast-top-right';
  toastrConfig.preventDuplicates = false;
  toastrConfig.progressBar = false;

  cfpLoadingBarProvider.includeBar = true;
  cfpLoadingBarProvider.includeSpinner = false;
  cfpLoadingBarProvider.latencyThreshold = 500;

  // alternatively, register the interceptor via an anonymous factory
  $httpProvider.interceptors.push(($q) => {
    return {
      'responseError': (rejection) => {
        if (rejection.status < 0) {
          rejection.data = {
            code: 5000,
            msg: 'Server Unavailable!'
          };
        }
        return $q.reject(rejection);
      }
    };
  });

}
