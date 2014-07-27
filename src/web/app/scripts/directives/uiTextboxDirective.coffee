angular.module("app").directive("uiTextbox", () ->
  restrict: "E"
  replace: true
  scope:
    value: "="
    label: "@"
    placeholder: "@"
  templateUrl: "/scripts/directives/uiTextboxDirective.html"
)
