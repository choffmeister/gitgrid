angular.module("app").controller("showProjectController", ["$scope", "$routeParams", "$data", "restService", ($scope, $routeParams, $data, restService) ->
  $scope.ownerName = $routeParams.ownerName
  $scope.projectName = $routeParams.projectName
  $scope.project = $data.project.data
  $scope.commits = []

  $scope.loadingCommits = false
  $scope.hasAllCommits = false
  $scope.dropCommits = 0

  $scope.loadCommits = () ->
    $scope.loadingCommits = true
    restService.listGitCommits($routeParams.ownerName, $routeParams.projectName, $scope.dropCommits, 25).then((res) ->
      if res.data.length > 0
        $scope.commits = $scope.commits.concat(res.data)
        $scope.dropCommits += res.data.length
      if res.data.length < 25
        $scope.hasAllCommits = true
      $scope.loadingCommits = false
    )
])

angular.module("app").factory("showProjectController$Data", ["restService", (restService) -> ($routeParams) ->
  project: restService.retrieveProject($routeParams.ownerName, $routeParams.projectName)
])
