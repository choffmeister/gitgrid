angular.module("app").config(["$routeProvider", "$locationProvider", ($routeProvider, $locationProvider) ->
  $routeProvider
    .when "/", { templateUrl: "/views/home.html" }
    .when "/about", { templateUrl: "/views/about.html" }
    .otherwise { templateUrl: "/views/notfound.html" }

  $locationProvider.html5Mode(true)
])
