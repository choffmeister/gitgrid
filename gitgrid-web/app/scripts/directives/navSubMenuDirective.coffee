angular.module("app").directive("navSubMenu", () ->
  restrict: "E"
  replace: true
  transclude: true
  scope:
    label: "="
  templateUrl: "/scripts/directives/navSubMenuDirective.html"
)
