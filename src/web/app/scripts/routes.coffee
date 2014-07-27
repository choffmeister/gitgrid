angular.module("app").config(["$routeProvider", "$locationProvider", ($routeProvider, $locationProvider) ->
  $routeProvider
    .when "/", { templateUrl: "/views/home.html", controller: "homeController" }
    .when "/login", { templateUrl: "/views/login.html", controller: "loginController" }
    .when "/logout", { resolve: { logout: ["authService", (authService) -> authService.logout()] }, redirectTo: "/" }
    .when "/register", { templateUrl: "/views/register.html", controller: "registerController" }
    .when "/users", { templateUrl: "/views/users.html", controller: "usersController" }
    .when "/new", { templateUrl: "/views/createproject.html", controller: "createProjectController" }
    .when "/about", { templateUrl: "/views/about.html" }
    .when "/_test-ui", { templateUrl: "/views/_test-ui.html" }

    .when "/:userName", { templateUrl: "/views/showuser.html", controller: "showUserController" }

    .when "/:ownerName/:projectName", { templateUrl: "/views/showproject.html", controller: "showProjectController" }
    .when "/:ownerName/:projectName/tree/:ref", { templateUrl: "/views/showprojecttree.html", controller: "showProjectTreeController" }
    .when "/:ownerName/:projectName/tree/:ref/:path*", { templateUrl: "/views/showprojecttree.html", controller: "showProjectTreeController" }
    .when "/:ownerName/:projectName/blob/:ref/:path*", { templateUrl: "/views/showprojectblob.html", controller: "showProjectBlobController" }

    .otherwise { templateUrl: "/views/notfound.html" }

  $locationProvider.html5Mode(true)
])
