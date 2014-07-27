angular.module("app").directive("uiGravatar", () ->
  restrict: "E"
  replace: true
  scope:
    email: "="
    size: "@"
  link: ($scope, element, attrs) ->
    generateLink = (email, size) ->
      if email?
        protocol = window.location.protocol
        md5 = CryptoJS.MD5(email)
        "#{protocol}//www.gravatar.com/avatar/#{md5}.jpg?s=#{size}&d=identicon"
      else
        ""
    $scope.$watch "email", (newValue) -> $scope.imageUrl = generateLink(newValue, $scope.size)
  templateUrl: "/scripts/directives/uiGravatarDirective.html"
)
