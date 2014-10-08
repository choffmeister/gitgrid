angular.module("app").service("restService", ["$http", "authService", ($http, authService) ->
  registerUser: (userName, email, password) ->
    $http.post("/api/users/register", { userName: userName, email: email, password: password })
  listUsers: () ->
    $http.get("/api/users")
  retrieveUser: (userName) ->
    $http.get("/api/users/#{userName}")
  listProjects: () ->
    $http.get("/api/projects")
  listProjectsForOwner: (ownerName) ->
    $http.get("/api/projects/#{ownerName}")
  retrieveProject: (ownerName, projectName) ->
    $http.get("/api/projects/#{ownerName}/#{projectName}")
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
  listGitCommits: (ownerName, projectName, ref) ->
    if not ref?
      $http.get("/api/projects/#{ownerName}/#{projectName}/git/commits")
    else
      $http.get("/api/projects/#{ownerName}/#{projectName}/git/commits/#{ref}")
  listGitBranches: (ownerName, projectName) ->
    $http.get("/api/projects/#{ownerName}/#{projectName}/git/branches")
  listGitTags: (ownerName, projectName) ->
    $http.get("/api/projects/#{ownerName}/#{projectName}/git/tags")
  retrieveGitTree: (ownerName, projectName, ref, path) ->
    $http.get("/api/projects/#{ownerName}/#{projectName}/git/tree/#{ref}/#{path}")
  retrieveGitBlob: (ownerName, projectName, ref, path) ->
    $http.get("/api/projects/#{ownerName}/#{projectName}/git/blob/#{ref}/#{path}")
  retrieveGitBlobRaw: (ownerName, projectName, ref, path) ->
    $http.get("/api/projects/#{ownerName}/#{projectName}/git/blob-raw/#{ref}/#{path}",
      transformResponse: (res) -> res
    )
])
