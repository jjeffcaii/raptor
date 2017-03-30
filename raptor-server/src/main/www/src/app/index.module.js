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
    endpoint: 'http://127.0.0.1:8080',
    headers: {
      'x-ml-appid': '56a86320e9db7300015438f7',
      'x-ml-apikey': 'iseedeadpeople'
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
