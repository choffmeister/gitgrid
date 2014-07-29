angular.module("app").config(["$routeProvider", "$locationProvider", ($routeProvider, $locationProvider) ->
  route = (url, templateUrl, controller, opts) ->
    options =
      templateUrl: templateUrl + ".html"
      controller: controller
      resolve:
        $data: ["$injector", "$q", "$route", ($injector, $q, $route) ->
          angular.module("app").value("$routeParams", $route.current.params)
          inj = angular.injector(["app"])

          switch inj.has("#{controller}$Data")
            when true then $q.all(inj.get("#{controller}$Data"))
            else undefined
        ]
    $routeProvider.when url, _.extend({}, options, opts)

  route("/", "/views/home", "homeController")
  route("/login", "/views/login", "loginController")
  route("/logout", undefined, undefined, { resolve: { logout: ["authService", (authService) -> authService.logout()] }, redirectTo: "/" })
  route("/register", "/views/register", "registerController")
  route("/new", "/views/createproject", "createProjectController")
  route("/about", "/views/about")

  route("/users", "/views/users", "usersController")
  route("/:userName", "/views/showuser", "showUserController")

  route("/:ownerName/:projectName", "/views/showproject", "showProjectController")
  route("/:ownerName/:projectName/tree/:ref", "/views/showprojecttree", "showProjectTreeController")
  route("/:ownerName/:projectName/tree/:ref/:path*", "/views/showprojecttree", "showProjectTreeController")
  route("/:ownerName/:projectName/blob/:ref/:path*", "/views/showprojectblob", "showProjectBlobController")

  $routeProvider.otherwise({ templateUrl: "/views/notfound.html" })
  $locationProvider.html5Mode(true)
])
