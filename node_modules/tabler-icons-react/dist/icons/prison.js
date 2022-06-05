import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Prison(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-prison",
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
    d: "M18 4v16"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 4v16"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M6 4v5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M6 15v5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10 4v5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11 9h-6v6h6z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10 15v5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 12h-.01"
  }));
}

export { Prison as default };
