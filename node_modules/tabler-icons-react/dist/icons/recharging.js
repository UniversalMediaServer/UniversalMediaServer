import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Recharging(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-recharging",
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
    d: "M7.038 4.5a9 9 0 0 0 -2.495 2.47"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3.186 10.209a9 9 0 0 0 0 3.508"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4.5 16.962a9 9 0 0 0 2.47 2.495"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10.209 20.814a9 9 0 0 0 3.5 0"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16.962 19.5a9 9 0 0 0 2.495 -2.47"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20.814 13.791a9 9 0 0 0 0 -3.508"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M19.5 7.038a9 9 0 0 0 -2.47 -2.495"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M13.791 3.186a9 9 0 0 0 -3.508 -.02"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 8l-2 4h4l-2 4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 21a9 9 0 0 0 0 -18"
  }));
}

export { Recharging as default };
