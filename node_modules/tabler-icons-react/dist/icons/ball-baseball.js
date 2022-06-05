import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function BallBaseball(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-ball-baseball",
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
    d: "M5.636 18.364a9 9 0 1 0 12.728 -12.728a9 9 0 0 0 -12.728 12.728z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12.495 3.02a9 9 0 0 1 -9.475 9.475"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20.98 11.505a9 9 0 0 0 -9.475 9.475"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 9l2 2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M13 13l2 2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11 7l2 1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M7 11l1 2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 11l1 2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11 16l2 1"
  }));
}

export { BallBaseball as default };
