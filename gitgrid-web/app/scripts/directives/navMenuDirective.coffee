angular.module("app").directive("navMenu", () ->
  restrict: "E"
  replace: true
  transclude: true
  scope:
    id: "@"
    brand: "@"
  templateUrl: "/scripts/directives/navMenuDirective.html"
)
