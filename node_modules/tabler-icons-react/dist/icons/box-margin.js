import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function BoxMargin(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-box-margin",
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
    d: "M8 8h8v8h-8z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4 4v.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 4v.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 4v.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 4v.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20 4v.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4 20v.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 20v.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 20v.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 20v.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20 20v.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20 16v.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20 12v.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20 8v.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4 16v.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4 12v.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4 8v.01"
  }));
}

export { BoxMargin as default };
