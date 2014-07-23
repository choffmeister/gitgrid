angular.module("app").controller("createProjectController", ["$scope", "$location", "restService", ($scope, $location, restService) ->
  $scope.name = ""
  $scope.description = ""
  $scope.isPublic = false
  $scope.message = null

  $scope.create = () ->
    restService.createProject($scope.name, $scope.description, $scope.isPublic)
      .success((res) -> $location.path("/#{res.ownerName}/#{res.name}").replace(true))
      .error((err) ->
        $scope.message =
          type: "error"
          text: "An unknown error occured"
      )
])
