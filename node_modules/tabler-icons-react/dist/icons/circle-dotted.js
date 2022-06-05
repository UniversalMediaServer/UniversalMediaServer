import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function CircleDotted(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-circle-dotted",
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
  }), /*#__PURE__*/React.createElement("line", {
    x1: "7.5",
    y1: "4.21",
    x2: "7.5",
    y2: "4.22"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "4.21",
    y1: "7.5",
    x2: "4.21",
    y2: "7.51"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "3",
    y1: "12",
    x2: "3",
    y2: "12.01"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "4.21",
    y1: "16.5",
    x2: "4.21",
    y2: "16.51"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "7.5",
    y1: "19.79",
    x2: "7.5",
    y2: "19.8"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "12",
    y1: "21",
    x2: "12",
    y2: "21.01"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "16.5",
    y1: "19.79",
    x2: "16.5",
    y2: "19.8"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "19.79",
    y1: "16.5",
    x2: "19.79",
    y2: "16.51"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "21",
    y1: "12",
    x2: "21",
    y2: "12.01"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "19.79",
    y1: "7.5",
    x2: "19.79",
    y2: "7.51"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "16.5",
    y1: "4.21",
    x2: "16.5",
    y2: "4.22"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "12",
    y1: "3",
    x2: "12",
    y2: "3.01"
  }));
}

export { CircleDotted as default };
