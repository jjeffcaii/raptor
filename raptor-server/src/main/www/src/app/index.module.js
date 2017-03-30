/* global moment:false */

import {config} from "./index.config";
import {routerConfig} from "./index.route";
import {runBlock} from "./index.run";
import {NavbarDirective} from "../app/components/navbar/navbar.directive";
import {GroupController} from "./groups/groups.controller";

angular.module('www', ['ngAnimate', 'ngCookies', 'ngTouch', 'ngSanitize', 'ngMessages', 'ngAria', 'ui.router', 'mgcrea.ngStrap', 'toastr'])
  .constant('moment', moment)
  .config(config)
  .config(routerConfig)
  .run(runBlock)
  .controller('GroupController', GroupController)
  .directive('raptorNavbar', NavbarDirective);
