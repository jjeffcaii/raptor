export function config($logProvider, toastrConfig, cfpLoadingBarProvider, $httpProvider) {
  'ngInject';
  // Enable log
  $logProvider.debugEnabled(true);

  // Set options third-party lib
  toastrConfig.allowHtml = true;
  toastrConfig.timeOut = 3000;
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
        return $q.reject(rejection);
      }
    };
  });

}
