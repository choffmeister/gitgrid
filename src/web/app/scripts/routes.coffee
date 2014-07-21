angular.module("app").config(["$routeProvider", "$locationProvider", ($routeProvider, $locationProvider) ->
  $routeProvider
    .when "/", { templateUrl: "/views/home.html", controller: "homeController" }
    .when "/login", { templateUrl: "/views/login.html", controller: "loginController" }
    .when "/logout", { templateUrl: "/views/logout.html", controller: "logoutController" }
    .when "/register", { templateUrl: "/views/register.html", controller: "registerController" }
    .when "/users", { templateUrl: "/views/users.html", controller: "usersController" }
    .when "/about", { templateUrl: "/views/about.html" }
    .otherwise { templateUrl: "/views/notfound.html" }

  $locationProvider.html5Mode(true)
])
