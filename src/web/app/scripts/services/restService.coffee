angular.module("app").factory("restService", ["$http", "authService", ($http, authService) ->
  register: (userName, password) ->
    $http.post("/api/auth/register", { userName: userName, password: password })
  listUsers: () ->
    $http.get("/api/users")
  listProjects: () ->
    $http.get("/api/projects")
  listProjectsForOwner: (ownerName) ->
    $http.get("/api/projects/#{ownerName}")
  createProject: (name, description, isPublic) ->
    $http.post("/api/projects",
      id: "000000000000000000000000",
      ownerId: authService.getUser().id,
      name: name,
      ownerName: "",
      description: description,
      public: isPublic,
      createdAt: 0,
      updatedAt: 0
    )
])
