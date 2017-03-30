export function routerConfig($stateProvider, $urlRouterProvider) {
  'ngInject';
  $stateProvider
    .state('home', {
      url: '/',
      templateUrl: 'app/main/main.html',
      controller: 'MainController',
      controllerAs: 'main'
    })
    .state('groups', {
      url: '/groups',
      templateUrl: 'app/groups/groups.html',
      controller: 'GroupController',
      controllerAs: 'group'
    });

  $urlRouterProvider.otherwise('/');
}
