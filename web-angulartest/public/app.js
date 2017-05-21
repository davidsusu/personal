
var app = angular.module('AngularTestApp',[]);

app.controller("MainController", function ($scope) {
    $scope.searchText = "Ot";
    $scope.text = "& character's HTML code is '&amp;'";
    $scope.items = [
        {"value": "val1", "label": "Item 1"},
        {"value": "val2", "label": "Item 2"},
        {"value": "val3", "label": "Item 3"},
        {"value": "other", "label": "Other item"}
    ];
    $scope.selectedValue = "val2";
    $scope.filterSearch = function (searchText) {
        return function (item) {
            if ($scope.searchText == "") {
            return true;
            }
            return item.label.substring(0, searchText.length) == searchText;
        };
    };
});

app.controller("TemplateController", function ($scope) {
    $scope.name = "Template";
});

app.directive('someDirective', function () {
    return {
        template: '<p>This is a directive, {{num1}} + {{num2}} = {{num1 + num2}}</p>'
    };
});

app.controller("DirectiveController", function ($scope) {
    $scope.num1 = 25;
    $scope.num2 = 32;
});
