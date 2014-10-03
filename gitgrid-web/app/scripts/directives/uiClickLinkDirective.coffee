angular.module("app").directive("uiClickLink", ["$location", ($location) ->
  restrict: "A"
  link: (scope, element, attrs) ->
    element.css("cursor", "pointer")
    element.on("click", ->
      scope.$apply(() -> $location.path(attrs.uiClickLink))
    )
])
