angular.module("app").controller("showProjectController", ["$scope", "$routeParams", "$data", "restService", ($scope, $routeParams, $data, restService) ->
  $scope.ownerName = $routeParams.ownerName
  $scope.projectName = $routeParams.projectName
  $scope.project = $data.project.data
  $scope.commits = []

  dropCommits = 0
  loadingCommits = false
  $scope.loadCommits = () -> if not loadingCommits
    loadingCommits = true
    restService.listGitCommits($routeParams.ownerName, $routeParams.projectName, dropCommits, 25).then((res) ->
      if res.data.length > 0
        $scope.commits = $scope.commits.concat(res.data)
        dropCommits += res.data.length
      if res.data.length >= 25
        loadingCommits = false
    )
])

angular.module("app").factory("showProjectController$Data", ["restService", (restService) -> ($routeParams) ->
  project: restService.retrieveProject($routeParams.ownerName, $routeParams.projectName)
])
