angular.module("app").directive("navEntry", () ->
  restrict: "E"
  replace: true
  scope:
    label: "="
    url: "="
  templateUrl: "/scripts/directives/navEntryDirective.html"
)
