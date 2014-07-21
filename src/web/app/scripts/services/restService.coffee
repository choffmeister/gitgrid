angular.module("app").factory("restService", ["$http", ($http) ->
  login: (userName, password) ->
    $http.post("/api/auth/login", { user: userName, pass: password })
  logout: () ->
    $http.post("/api/auth/logout", {})
  register: (userName, password) ->
    $http.post("/api/auth/register", { userName: userName, password: password })
  listUsers: () ->
    $http.get("/api/users")
  listProjects: () ->
    $http.get("/api/projects")
  listProjectsForOwner: (ownerName) ->
    $http.get("/api/projects/#{ownerName}")
])
