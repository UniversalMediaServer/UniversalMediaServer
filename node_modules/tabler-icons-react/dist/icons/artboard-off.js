import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function ArtboardOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-artboard-off",
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
    d: "M12 8h3a1 1 0 0 1 1 1v3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M15.716 15.698a0.997 .997 0 0 1 -.716 .302h-6a1 1 0 0 1 -1 -1v-6c0 -.273 .11 -.52 .287 -.7"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 8h1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 16h1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 3v1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 3v1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20 8h1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20 16h1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 20v1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 20v1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { ArtboardOff as default };
