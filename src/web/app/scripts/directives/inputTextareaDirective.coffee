angular.module("app").directive("inputTextarea", () ->
  restrict: "E"
  replace: true
  scope:
    value: "="
    label: "@"
    placeholder: "@"
    rows: "@"
  template: """<div class="form-group">
      <label class="control-label">{{label}}</label>
      <textarea class="form-control" placeholder="{{placeholder}}" rows="{{rows}}" ng-model="value"></textarea>
    </div>"""
)
