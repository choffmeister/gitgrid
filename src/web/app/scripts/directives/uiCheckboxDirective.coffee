angular.module("app").directive("uiCheckbox", () ->
  restrict: "E"
  replace: true
  scope:
    value: "="
    disabled: "="
    label: "@"
  templateUrl: "/scripts/directives/uiCheckboxDirective.html"
)
