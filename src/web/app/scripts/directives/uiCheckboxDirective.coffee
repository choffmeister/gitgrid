angular.module("app").directive("uiCheckbox", () ->
  restrict: "E"
  replace: true
  scope:
    value: "="
    label: "@"
  templateUrl: "/scripts/directives/uiCheckboxDirective.html"
)
