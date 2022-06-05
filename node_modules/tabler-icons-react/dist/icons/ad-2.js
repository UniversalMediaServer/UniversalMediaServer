import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Ad2(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-ad-2",
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
    d: "M11.933 5h-6.933v16h13v-8"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 17h-5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 13h5v-4h-5z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M15 5v-2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M18 6l2 -2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M19 9h2"
  }));
}

export { Ad2 as default };
