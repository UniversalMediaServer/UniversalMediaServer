import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function BarrelOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-barrel-off",
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    stroke: color,
    strokeWidth: "2",
    fill: "none",
    strokeLinecap: "round",
    strokeLinejoin: "round"
  }, restProps), /*#__PURE__*/React.createElement("path", {
    stroke: "none",
    d: "M0 0h24v24H0z",
    fill: "none"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 4h8.722a2 2 0 0 1 1.841 1.22c.958 2.26 1.437 4.52 1.437 6.78a16.35 16.35 0 0 1 -.407 3.609m-.964 3.013l-.066 .158a2 2 0 0 1 -1.841 1.22h-9.444a2 2 0 0 1 -1.841 -1.22c-.958 -2.26 -1.437 -4.52 -1.437 -6.78c0 -2.21 .458 -4.42 1.374 -6.63"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 4c.585 2.337 .913 4.674 .985 7.01m-.114 3.86a33.415 33.415 0 0 1 -.871 5.13"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10 4a34.42 34.42 0 0 0 -.366 1.632m-.506 3.501a32.126 32.126 0 0 0 -.128 2.867c0 2.667 .333 5.333 1 8"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4.5 16h11.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M19.5 8h-7.5m-4 0h-3.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { BarrelOff as default };
