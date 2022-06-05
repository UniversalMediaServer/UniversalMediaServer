import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function ThreeDCubeSphere(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-3d-cube-sphere",
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
    d: "M6 17.6l-2 -1.1v-2.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4 10v-2.5l2 -1.1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10 4.1l2 -1.1l2 1.1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M18 6.4l2 1.1v2.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20 14v2.5l-2 1.12"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 19.9l-2 1.1l-2 -1.1"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "12",
    y1: "12",
    x2: "14",
    y2: "10.9"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "18",
    y1: "8.6",
    x2: "20",
    y2: "7.5"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "12",
    y1: "12",
    x2: "12",
    y2: "14.5"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "12",
    y1: "18.5",
    x2: "12",
    y2: "21"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 12l-2 -1.12"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "6",
    y1: "8.6",
    x2: "4",
    y2: "7.5"
  }));
}

export { ThreeDCubeSphere as default };
