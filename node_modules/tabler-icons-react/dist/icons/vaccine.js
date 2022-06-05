import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Vaccine(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-vaccine",
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
    d: "M17 3l4 4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M19 5l-4.5 4.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11.5 6.5l6 6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16.5 11.5l-6.5 6.5h-4v-4l6.5 -6.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M7.5 12.5l1.5 1.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10.5 9.5l1.5 1.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 21l3 -3"
  }));
}

export { Vaccine as default };
