/* global moment:false */

import {config} from "./index.config";
import {routerConfig} from "./index.route";
import {runBlock} from "./index.run";
import {NavbarDirective} from "../app/components/navbar/navbar.directive";
import {GroupController} from "./groups/groups.controller";
import {MainController} from "./main/main.controller";

angular.module('www', ['ngAnimate', 'ngCookies', 'ngTouch', 'ngSanitize', 'ngMessages', 'ngAria', 'ui.router', 'mgcrea.ngStrap', 'toastr', 'angular-loading-bar'])
  .constant('R', {
    endpoint: 'http://127.0.0.1:8080',
    auth: {
      ns: '56a86320e9db7300015438f7',
      pwd: 'iseedeadpeople'
    }
  })
  .constant('moment', moment)
  .config(config)
  .config(routerConfig)
  .run(runBlock)
  .controller('MainController', MainController)
  .controller('GroupController', GroupController)
  .directive('raptorNavbar', NavbarDirective);
