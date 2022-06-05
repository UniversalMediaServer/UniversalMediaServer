import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function FileZip(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-file-zip",
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
    d: "M6 20.735a2 2 0 0 1 -1 -1.735v-14a2 2 0 0 1 2 -2h7l5 5v11a2 2 0 0 1 -2 2h-1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M11 17a2 2 0 0 1 2 2v2a1 1 0 0 1 -1 1h-2a1 1 0 0 1 -1 -1v-2a2 2 0 0 1 2 -2z"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "11",
    y1: "5",
    x2: "10",
    y2: "5"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "13",
    y1: "7",
    x2: "12",
    y2: "7"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "11",
    y1: "9",
    x2: "10",
    y2: "9"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "13",
    y1: "11",
    x2: "12",
    y2: "11"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "11",
    y1: "13",
    x2: "10",
    y2: "13"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "13",
    y1: "15",
    x2: "12",
    y2: "15"
  }));
}

export { FileZip as default };
