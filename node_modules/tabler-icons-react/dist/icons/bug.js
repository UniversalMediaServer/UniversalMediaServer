import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Bug(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-bug",
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
    d: "M9 9v-1a3 3 0 0 1 6 0v1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 9h8a6 6 0 0 1 1 3v3a5 5 0 0 1 -10 0v-3a6 6 0 0 1 1 -3"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "3",
    y1: "13",
    x2: "7",
    y2: "13"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "17",
    y1: "13",
    x2: "21",
    y2: "13"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "12",
    y1: "20",
    x2: "12",
    y2: "14"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "4",
    y1: "19",
    x2: "7.35",
    y2: "17"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "20",
    y1: "19",
    x2: "16.65",
    y2: "17"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "4",
    y1: "7",
    x2: "7.75",
    y2: "9.4"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "20",
    y1: "7",
    x2: "16.25",
    y2: "9.4"
  }));
}

export { Bug as default };
