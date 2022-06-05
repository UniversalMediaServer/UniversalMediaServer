import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Fingerprint(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-fingerprint",
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
    d: "M18.9 7a8 8 0 0 1 1.1 5v1a6 6 0 0 0 .8 3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 11a4 4 0 0 1 8 0v1a10 10 0 0 0 2 6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 11v2a14 14 0 0 0 2.5 8"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 15a18 18 0 0 0 1.8 6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4.9 19a22 22 0 0 1 -.9 -7v-1a8 8 0 0 1 12 -6.95"
  }));
}

export { Fingerprint as default };
