import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function AntennaBars4(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-antenna-bars-4",
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
    x1: "6",
    y1: "18",
    x2: "6",
    y2: "15"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "10",
    y1: "18",
    x2: "10",
    y2: "12"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "14",
    y1: "18",
    x2: "14",
    y2: "9"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "18",
    y1: "18",
    x2: "18",
    y2: "18.01"
  }));
}

export { AntennaBars4 as default };
