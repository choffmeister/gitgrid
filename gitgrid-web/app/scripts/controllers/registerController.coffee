angular.module("app").controller("registerController", ["$scope", "$location", "flashService", "restService", ($scope, $location, flashService, restService) ->
  $scope.userName = ""
  $scope.email = ""
  $scope.password = ""
  $scope.message = null

  $scope.register = () ->
    restService.registerUser($scope.userName, $scope.email, $scope.password)
      .success((res) ->
        flashService.success("Your registration was successful.")
        $scope.password = ""
        $location.path("/login")
      )
      .error((err) ->
        $scope.password = ""
        $scope.message =
          type: "error"
          text: "An unknown error occured"
      )
])
