angular.module("app").config(["$routeProvider", "$locationProvider", ($routeProvider, $locationProvider) ->
  $routeProvider
    .when "/", { templateUrl: "/views/home.html", controller: "homeController" }
    .when "/login", { templateUrl: "/views/login.html", controller: "loginController" }
    .when "/logout", { resolve: { logout: ["authService", (authService) -> authService.logout()] }, redirectTo: "/" }
    .when "/register", { templateUrl: "/views/register.html", controller: "registerController" }
    .when "/users", { templateUrl: "/views/users.html", controller: "usersController" }
    .when "/new", { templateUrl: "/views/createproject.html", controller: "createProjectController" }
    .when "/about", { templateUrl: "/views/about.html" }

    .when "/:userName", { templateUrl: "/views/showuser.html", controller: "showUserController" }
    .when "/:ownerName/:projectName", { templateUrl: "/views/showproject.html", controller: "showProjectController" }

    .otherwise { templateUrl: "/views/notfound.html" }

  $locationProvider.html5Mode(true)
])
