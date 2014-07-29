angular.module("app").controller("createProjectController", ["$scope", "$location", "flashService", "restService", ($scope, $location, flashService, restService) ->
  $scope.name = ""
  $scope.description = ""
  $scope.isPublic = false
  $scope.message = null

  $scope.create = () ->
    restService.createProject($scope.name, $scope.description, $scope.isPublic)
      .success((res) ->
        flashService.success("You created a new project.")
        $location.path("/#{res.ownerName}/#{res.name}").replace(true)
      )
      .error((err) ->
        $scope.message =
          type: "error"
          text: "An unknown error occured"
      )
])
