angular.module("app").directive("uiTextarea", () ->
  restrict: "E"
  replace: true
  scope:
    value: "="
    label: "@"
    placeholder: "@"
    rows: "@"
  templateUrl: "/scripts/directives/uiTextareaDirective.html"
)
