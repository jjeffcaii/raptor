/* global moment:false */

import {config} from "./index.config";
import {routerConfig} from "./index.route";
import {runBlock} from "./index.run";
import {NavbarDirective} from "../app/components/navbar/navbar.directive";
import {GroupController} from "./groups/groups.controller";
import {MainController} from "./main/main.controller";
import {GpDirective} from "./components/gp/gp.directive";

angular.module('www', ['ngAnimate', 'ngCookies', 'ngTouch', 'ngSanitize', 'ngMessages', 'ngAria', 'ui.router', 'toastr', 'angular-loading-bar', 'monospaced.qrcode'])
  .constant('R', {
    endpoint: '../',
    headers: {
      'X-ML-AppId': '5795ad33aa150a0001fcbfa3',
      'X-ML-APIKey': 'QXNJZnpNRDBZejhfbmpwRjlBVk5Bdw',
      'Content-Type': 'application/json; charset=utf-8'
    }
  })
  .constant('moment', moment)
  .config(config)
  .config(routerConfig)
  .run(runBlock)
  .controller('MainController', MainController)
  .controller('GroupController', GroupController)
  .directive('raptorGroup', GpDirective)
  .directive('raptorNavbar', NavbarDirective);
